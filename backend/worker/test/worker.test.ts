import { describe, expect, it } from "vitest";
import { sanitize } from "../src/ai";
import { layout, templates } from "../src/email";
import { Env, HttpError } from "../src/env";
import { route } from "../src/index";
import { blob, ownsKey, signedUrl, uploadUrl } from "../src/media";
import { assess, worst } from "../src/safety";

const memoryBucket = () => {
  const store = new Map<string, string>();
  return {
    store,
    async put(k: string, v: ReadableStream | string | null) { store.set(k, typeof v === "string" ? v : await new Response(v).text()); },
    async get(k: string) { return store.has(k) ? { body: store.get(k) } : null; },
    async delete(k: string | string[]) { (Array.isArray(k) ? k : [k]).forEach((x) => store.delete(x)); },
    async list({ prefix }: { prefix: string }) { return { objects: [...store.keys()].filter((k) => k.startsWith(prefix)).map((key) => ({ key })), truncated: false }; },
  };
};

const env = (over: Partial<Env> = {}): Env => ({
  MEDIA: memoryBucket() as unknown as R2Bucket,
  RATE: { get: async () => null, put: async () => undefined } as unknown as KVNamespace,
  FIREBASE_PROJECT_ID: "tuulit-test", FIREBASE_PROJECT_NUMBER: "123", APP_CHECK_MODE: "off",
  AI_PROVIDER: "anthropic", AI_MODEL: "test", AI_DAILY_LIMIT: "60", BREVO_SENDER_EMAIL: "hello@example.com",
  BREVO_SENDER_NAME: "Tuulit", APP_NAME: "Tuulit", PUBLIC_BASE_URL: "https://api.example.com",
  AI_API_KEY: "", BREVO_API_KEY: "", FIREBASE_SERVICE_ACCOUNT: "", MEDIA_SIGNING_KEY: "k".repeat(40), ...over,
});

describe("safety", () => {
  it("matches the app's rules", () => {
    expect(assess("I fainted and I'm bleeding")).toBe("EMERGENCY");
    expect(assess("soaking through a pad every hour")).toBe("URGENT");
    expect(assess("why am I bloated")).toBe("NONE");
    expect(assess("Biology 101 hormones")).toBe("NONE");
    expect(worst("URGENT", "NONE")).toBe("URGENT");
  });
});

describe("AI request sanitising", () => {
  it("rejects unknown tools and empty questions", () => {
    expect(() => sanitize({ tool: "hack", messages: [{ role: "user", content: "hi" }] })).toThrow(HttpError);
    expect(() => sanitize({ tool: "chat", messages: [] })).toThrow(HttpError);
    expect(() => sanitize({ tool: "chat", messages: [{ role: "assistant", content: "hi" }] })).toThrow(HttpError);
  });
  it("trims history and message length", () => {
    const messages = Array.from({ length: 30 }, (_, i) => ({ role: i % 2 ? "assistant" : "user", content: "x".repeat(5000) }));
    messages.push({ role: "user", content: "last" });
    const r = sanitize({ tool: "chat", messages });
    expect(r.messages.length).toBeLessThanOrEqual(12);
    expect(r.messages.every((m) => m.content.length <= 2000)).toBe(true);
    expect(r.messages[r.messages.length - 1].content).toBe("last");
  });
});

describe("media", () => {
  it("only lets a user address their own keys", () => {
    expect(ownsKey("abc", "users/abc/voice/1.m4a")).toBe(true);
    expect(ownsKey("abc", "users/xyz/voice/1.m4a")).toBe(false);
    expect(ownsKey("abc", "users/abc/../xyz/1.m4a")).toBe(false);
  });

  it("round-trips through a signed URL and rejects tampering", async () => {
    const e = env();
    const res = await uploadUrl(e, "abc", { kind: "voice", contentType: "audio/mp4", bytes: 4 });
    const { url, key } = (await res.json()) as { url: string; key: string };
    expect(key.startsWith("users/abc/voice/")).toBe(true);
    const put = await blob(e, new Request(url, { method: "PUT", body: "data", headers: { "content-type": "audio/mp4", "content-length": "4" } }));
    expect(put.status).toBe(200);
    await expect(blob(e, new Request(url.replace("sig=", "sig=x"), { method: "PUT", body: "data", headers: { "content-type": "audio/mp4", "content-length": "4" } }))).rejects.toThrow("Invalid link.");
    const getUrl = await signedUrl(e, "GET", key);
    const got = await blob(e, new Request(getUrl));
    expect(await got.text()).toBe("data");
    // A PUT signature can't be used to GET.
    await expect(blob(e, new Request(url))).rejects.toThrow("Invalid link.");
  });

  it("rejects bad uploads", async () => {
    await expect(uploadUrl(env(), "abc", { kind: "photo", contentType: "image/png", bytes: 4 })).rejects.toThrow(HttpError);
    await expect(uploadUrl(env(), "abc", { kind: "voice", contentType: "audio/mp4", bytes: 50 * 1024 * 1024 })).rejects.toThrow("under 10 MB");
  });
});

describe("routing", () => {
  it("serves health without auth and rejects unauthenticated API calls", async () => {
    expect((await route(new Request("https://api.example.com/health"), env())).status).toBe(200);
    await expect(route(new Request("https://api.example.com/v1/ai", { method: "POST", body: "{}" }), env())).rejects.toThrow("Sign in");
    await expect(route(new Request("https://api.example.com/v1/ai", { method: "GET" }), env())).rejects.toThrow("Method not allowed");
  });

  it("rejects forged bearer tokens", async () => {
    const forged = ["eyJhbGciOiJub25lIn0", "eyJzdWIiOiJ4In0", ""].join(".");
    await expect(route(new Request("https://api.example.com/v1/ai", { method: "POST", body: "{}", headers: { authorization: `Bearer ${forged}` } }), env()))
      .rejects.toThrow(HttpError);
  });
});

describe("email templates", () => {
  it("escape user input and carry no health data", () => {
    const w = templates.welcome("<script>alert(1)</script>");
    expect(w.html).not.toContain("<script>");
    expect(w.html).toContain("&lt;script&gt;");
    expect(layout("T", "<p>x</p>", { label: "Go", url: "https://x.test/?a=1&b=2" })).toContain("a=1&amp;b=2");
    for (const t of [templates.welcome("A"), templates.verify("https://x"), templates.deleted()]) {
      expect(t.html.toLowerCase()).not.toMatch(/period|cycle day|symptom|ovulat/);
    }
  });
});

describe("push messages", () => {
  it("keeps Android messages data-only and adds a discreet APNs alert for iOS", async () => {
    const { fcmMessage } = await import("../src/google");
    const data = { type: "sos", fromName: "Sam", message: "Cramps are bad", connectionId: "a_b" };
    const android = fcmMessage("t1", data, "android");
    expect(android).toEqual({ token: "t1", data, android: { priority: "HIGH" } });
    expect(fcmMessage("t0", data)).toEqual({ token: "t0", data, android: { priority: "HIGH" } });
    const ios = fcmMessage("t2", data, "ios") as { apns: { payload: { aps: { alert: { title: string; body: string }; "mutable-content": number } } } };
    expect(ios.apns.payload.aps.alert).toEqual({ title: "Tuulit", body: "Take a quick moment for yourself." });
    expect(ios.apns.payload.aps["mutable-content"]).toBe(1);
    expect(JSON.stringify(ios.apns)).not.toContain("Cramps");
  });
});
