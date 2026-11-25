import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc, deleteDoc, writeBatch, collection, query, where, getDocs } from "firebase/firestore";
import { readFileSync } from "node:fs";
import { afterAll, beforeAll, beforeEach, describe, it } from "vitest";

let env;
const OWNER = "ownerAAAAAAAAAA";
const MEMBER = "memberBBBBBBBBBB";
const STRANGER = "strangerCCCCCCCC";
const CONN = `${OWNER}_${MEMBER}`;
const offPerms = { periodSoon: false, periodStarted: false, roughDay: false, phase: false };

beforeAll(async () => {
  env = await initializeTestEnvironment({ projectId: "demo-tuulit", firestore: { rules: readFileSync("firestore.rules", "utf8") } });
});
afterAll(async () => env.cleanup());
beforeEach(async () => env.clearFirestore());

const db = (uid, verified = true) => (uid ? env.authenticatedContext(uid, { email_verified: verified }).firestore() : env.unauthenticatedContext().firestore());

async function seed(fn) {
  await env.withSecurityRulesDisabled(async (ctx) => fn(ctx.firestore()));
}

async function invite(code = "4K8P2Q", over = {}) {
  await seed((f) => setDoc(doc(f, "invites", code), { ownerUid: OWNER, ownerName: "Maya", status: "open", expiresAt: Date.now() + 3600_000, ...over }));
}

async function accept(uid = MEMBER, code = "4K8P2Q", perms = offPerms) {
  const f = db(uid);
  const b = writeBatch(f);
  b.set(doc(f, "connections", `${OWNER}_${uid}`), { ownerUid: OWNER, memberUid: uid, ownerName: "Maya", memberName: "Sam", relationship: "Partner", inviteCode: code, permissions: perms, createdAt: 1 });
  b.update(doc(f, "invites", code), { status: "accepted", acceptedBy: uid });
  return b.commit();
}

describe("private backup", () => {
  it("only the owner reads and writes their own data", async () => {
    await assertSucceeds(setDoc(doc(db(OWNER), "users", OWNER, "dailyLogs", "20000"), { epochDay: 20000, flow: "LIGHT", updatedAt: 1 }));
    await assertSucceeds(getDoc(doc(db(OWNER), "users", OWNER, "dailyLogs", "20000")));
    await assertFails(getDoc(doc(db(STRANGER), "users", OWNER, "dailyLogs", "20000")));
    await assertFails(setDoc(doc(db(STRANGER), "users", OWNER, "dailyLogs", "1"), { epochDay: 1 }));
    await assertFails(getDoc(doc(db(null), "users", OWNER)));
  });

  it("refuses a signed-in user whose email isn't confirmed, even for their own data", async () => {
    await seed((f) => setDoc(doc(f, "users", OWNER, "dailyLogs", "20000"), { epochDay: 20000, updatedAt: 1 }));
    await assertFails(getDoc(doc(db(OWNER, false), "users", OWNER, "dailyLogs", "20000")));
    await assertFails(setDoc(doc(db(OWNER, false), "users", OWNER, "dailyLogs", "1"), { epochDay: 1, updatedAt: 1 }));
    await assertFails(setDoc(doc(db(OWNER, false), "invites", "4K8P2Q"), { ownerUid: OWNER, ownerName: "Maya", status: "open", expiresAt: Date.now() + 3600_000 }));
  });

  it("rejects unknown collections and oversized notes", async () => {
    await assertFails(setDoc(doc(db(OWNER), "users", OWNER, "secrets", "x"), { a: 1 }));
    await assertFails(setDoc(doc(db(OWNER), "users", OWNER, "dailyLogs", "1"), { journalText: "x".repeat(4001) }));
  });
});

describe("invites and connections", () => {
  it("owner creates a valid invite; bad codes and other owners are refused", async () => {
    const f = db(OWNER);
    await assertSucceeds(setDoc(doc(f, "invites", "4K8P2Q"), { ownerUid: OWNER, ownerName: "Maya", status: "open", expiresAt: Date.now() + 3600_000 }));
    await assertFails(setDoc(doc(f, "invites", "ab"), { ownerUid: OWNER, ownerName: "Maya", status: "open", expiresAt: Date.now() + 3600_000 }));
    await assertFails(setDoc(doc(f, "invites", "4K8P2R"), { ownerUid: STRANGER, ownerName: "Maya", status: "open", expiresAt: Date.now() + 3600_000 }));
    await assertFails(setDoc(doc(f, "invites", "4K8P2S"), { ownerUid: OWNER, ownerName: "Maya", status: "open", expiresAt: Date.now() + 90 * 24 * 3600_000 }));
  });

  it("invitee accepts once, with every permission off", async () => {
    await invite();
    await assertFails(accept(MEMBER, "4K8P2Q", { ...offPerms, roughDay: true }));
    await assertSucceeds(accept());
    await assertFails(accept(STRANGER)); // already used
  });

  it("expired invites can't be accepted, and owners can't accept their own", async () => {
    await invite("4K8P2Q", { expiresAt: Date.now() - 1000 });
    await assertFails(accept());
    await invite("5K8P2Q");
    const f = db(OWNER);
    await assertFails(setDoc(doc(f, "connections", `${OWNER}_${OWNER}`), { ownerUid: OWNER, memberUid: OWNER, ownerName: "M", memberName: "M", relationship: "", inviteCode: "5K8P2Q", permissions: offPerms, createdAt: 1 }));
  });

  it("only the owner changes permissions; either side can disconnect; strangers see nothing", async () => {
    await invite(); await accept();
    await assertFails(updateDoc(doc(db(MEMBER), "connections", CONN), { permissions: { ...offPerms, phase: true } }));
    await assertSucceeds(updateDoc(doc(db(OWNER), "connections", CONN), { permissions: { ...offPerms, phase: true } }));
    await assertFails(updateDoc(doc(db(OWNER), "connections", CONN), { memberUid: STRANGER }));
    await assertFails(getDoc(doc(db(STRANGER), "connections", CONN)));
    await assertSucceeds(getDocs(query(collection(db(MEMBER), "connections"), where("memberUid", "==", MEMBER))));
    await assertFails(getDocs(collection(db(STRANGER), "connections")));
    await assertSucceeds(deleteDoc(doc(db(MEMBER), "connections", CONN)));
  });
});

describe("shared status", () => {
  it("owner can only publish fields they permitted; member can read; stranger cannot", async () => {
    await invite(); await accept();
    const f = db(OWNER);
    const base = { ownerUid: OWNER, memberUid: MEMBER, ownerName: "Maya", updatedAt: 1 };
    await assertSucceeds(setDoc(doc(f, "shared", CONN), { ...base, phase: null, periodStartedOn: null }));
    await assertFails(setDoc(doc(f, "shared", CONN), { ...base, phase: "Luteal" }));
    await assertFails(setDoc(doc(f, "shared", CONN), { ...base, symptoms: "Cramps" }));
    await updateDoc(doc(f, "connections", CONN), { permissions: { ...offPerms, phase: true, roughDay: true } });
    await assertSucceeds(setDoc(doc(f, "shared", CONN), { ...base, phase: "Luteal", roughDayAt: 5, lastNote: "Rough day" }));
    await assertSucceeds(getDoc(doc(db(MEMBER), "shared", CONN)));
    await assertFails(getDoc(doc(db(STRANGER), "shared", CONN)));
    await assertFails(setDoc(doc(db(MEMBER), "shared", CONN), { ...base, ownerUid: MEMBER }));
  });
});
