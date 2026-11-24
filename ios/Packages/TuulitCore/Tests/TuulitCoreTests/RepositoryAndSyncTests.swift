import XCTest
@testable import TuulitCore

/// Ports of Android `TrackingRepositoryTest`, plus cross-platform sync tests that feed
/// Android-shaped Firestore documents through the merge rules.
final class TrackingRepositoryTests: XCTestCase {
    var store: TuulitStore!
    var repo: TrackingRepository!
    var care: CarePlayRepository!
    var changes = 0
    var clock: Int64 = 1_000

    override func setUp() {
        store = .inMemory()
        changes = 0; clock = 1_000
        repo = TrackingRepository(store: store, onChange: { [unowned self] in self.changes += 1 }, now: { [unowned self] in self.clock += 1; return self.clock })
        care = CarePlayRepository(store: store, onChange: { [unowned self] in self.changes += 1 }, now: { [unowned self] in self.clock += 1; return self.clock })
    }

    func testLoggingFlowCreatesOneCycleAndExtendsIt() {
        repo.saveDailyLog(DailyLog(epochDay: 100, flow: .MEDIUM))
        repo.saveDailyLog(DailyLog(epochDay: 101, flow: .HEAVY))
        repo.saveDailyLog(DailyLog(epochDay: 102, flow: .LIGHT))
        XCTAssertEqual(repo.cycles.count, 1)
        XCTAssertEqual(repo.cycles[0].startEpochDay, 100)
        XCTAssertGreaterThanOrEqual(changes, 3)
    }

    func testSpottingDoesNotStartACycle() {
        repo.saveDailyLog(DailyLog(epochDay: 100, flow: .SPOTTING))
        XCTAssertTrue(repo.cycles.isEmpty)
    }

    func testLoggingADayBeforeMovesTheStartEarlier() {
        repo.saveDailyLog(DailyLog(epochDay: 100, flow: .MEDIUM))
        repo.saveDailyLog(DailyLog(epochDay: 98, flow: .LIGHT))
        XCTAssertEqual(repo.cycles.count, 1)
        XCTAssertEqual(repo.cycles[0].startEpochDay, 98)
    }

    func testRemovingTheOnlyFlowDayRemovesTheCycle() {
        repo.saveDailyLog(DailyLog(epochDay: 100, flow: .MEDIUM))
        repo.saveDailyLog(DailyLog(epochDay: 100, flow: .NONE, moodScore: 3))
        XCTAssertTrue(repo.cycles.isEmpty)
        XCTAssertEqual(repo.dailyLog(100)?.moodScore, 3)
    }

    func testSeparateMonthsMakeSeparateCyclesAndDuplicatesAreIgnored() {
        repo.logPeriodStart(100); repo.logPeriodStart(100); repo.logPeriodStart(128)
        XCTAssertEqual(repo.cycles.map(\.startEpochDay), [100, 128])
    }

    func testEmptyLogBecomesTombstoneNotRow() {
        repo.saveDailyLog(DailyLog(epochDay: 100, moodScore: 4))
        repo.saveDailyLog(DailyLog(epochDay: 100))
        XCTAssertNil(repo.dailyLog(100))
        XCTAssertTrue(repo.dailyLogs.isEmpty)
        XCTAssertEqual(store.read { $0.day(100)?.deleted }, true)
        repo.saveDailyLog(DailyLog(epochDay: 100, journalText: "private"))
        repo.deleteDailyLog(100)
        XCTAssertEqual(store.read { $0.day(100)?.journalText }, "", "tombstone keeps only the date")
    }

    func testEditCycleValidates() {
        repo.logPeriodStart(100); repo.logPeriodStart(130)
        let id = repo.cycles.first!.id
        XCTAssertNotNil(repo.editCycle(id: id, start: 105, end: 100, today: 200))
        XCTAssertNotNil(repo.editCycle(id: id, start: 127, end: 130, today: 200))
        XCTAssertNil(repo.editCycle(id: id, start: 99, end: 103, today: 200))
        XCTAssertEqual(repo.cycles.first!.startEpochDay, 99)
        repo.deleteCycle(id: id)
        XCTAssertEqual(repo.cycles.count, 1)
    }

    func testPastPeriodFromOnboardingHasNoFakeFlow() {
        repo.addPastPeriod(start: 90, typicalLength: 5, today: 100)
        XCTAssertEqual(repo.cycles.count, 1)
        XCTAssertEqual(repo.cycles[0].endEpochDay, 94)
        XCTAssertTrue(repo.dailyLogs.isEmpty)
    }

    func testPainLogsSaveDeleteAndRestore() {
        let p = PainLog(id: "p1", epochDay: 100, intensity: 12, location: "Lower back", painType: "", durationHours: 2, medicationTaken: "",
                        impactedWorkOrStudy: true, impactedExercise: false, impactedSleep: false, notes: "")
        repo.savePainLog(p)
        XCTAssertEqual(repo.painLogs.first?.intensity, 10) // clamped
        repo.deletePainLog(id: "p1")
        XCTAssertTrue(repo.painLogs.isEmpty)
        repo.savePainLog(p)
        XCTAssertEqual(repo.painLogs.count, 1)
    }

    func testCSVExportEscapesAndNeutralisesFormulas() {
        repo.saveDailyLog(DailyLog(epochDay: 100, flow: .LIGHT, symptoms: ["Cramps": .SEVERE], journalText: "=HYPERLINK(\"x\"), ok"))
        let csv = repo.exportCSV()
        XCTAssertTrue(csv.components(separatedBy: "\n").first!.hasPrefix("type,date"))
        XCTAssertTrue(csv.contains("\"'=HYPERLINK(\"\"x\"\"), ok\""), csv)
        XCTAssertTrue(csv.contains("Cramps (Severe)"))
    }

    func testSessionsCreditLibraryAndAchievementsOnce() {
        care.seedLibraryIfEmpty(); care.seedLibraryIfEmpty()
        XCTAssertEqual(care.comfortItems.filter { $0.kind == "kit" }.count, 4)
        let first = care.completeSession("slow_breathing", fully: true)
        XCTAssertTrue(first.contains(.FIRST_SESSION))
        care.completeSession("slow_breathing", fully: true)
        XCTAssertEqual(care.comfortItems.first { $0.sessionId == "slow_breathing" }?.helpedCount, 2)
        XCTAssertTrue(care.completeSession("slow_breathing", fully: false).isEmpty)
    }

    func testGameBestScoreOnlyGoesUp() {
        XCTAssertTrue(care.recordGame("banana_catch", score: 120))
        XCTAssertFalse(care.recordGame("banana_catch", score: 80))
        XCTAssertTrue(care.recordGame("banana_catch", score: 200))
        XCTAssertEqual(care.gameScores["banana_catch"]?.best, 200)
        XCTAssertEqual(care.gameScores["banana_catch"]?.plays, 3)
    }

    func testStorePersistsAndReloads() {
        let backend = MemoryBackend()
        let s1 = TuulitStore(backend: backend)
        TrackingRepository(store: s1).logPeriodStart(500)
        s1.flush()
        let s2 = TuulitStore(backend: backend)
        XCTAssertEqual(TrackingRepository(store: s2).cycles.map(\.startEpochDay), [500])
    }
}

final class SyncMergeTests: XCTestCase {
    /// A document exactly as Android's SyncRepository writes it (Firestore numbers arrive as NSNumber/Int64).
    private func androidDay(_ day: Int, flow: String, updatedAt: Int64, deleted: Bool = false) -> RemoteDoc {
        RemoteDoc(id: String(day), fields: [
            "epochDay": NSNumber(value: Int64(day)), "flow": flow, "symptoms": "Cramps:2|Bloating:1", "moods": "Calm|Tired",
            "moodScore": NSNumber(value: 4), "energyLevel": NSNumber(value: 2), "sleepQuality": NSNumber(value: 0),
            "waterGlasses": NSNumber(value: 3), "exerciseNote": "", "medicationNote": "ibuprofen", "journalText": "note",
            "voiceNoteKey": NSNull(), "updatedAt": NSNumber(value: updatedAt), "deleted": deleted])
    }

    func testAndroidDailyLogDecodesIdentically() {
        var d = TuulitData()
        var r = RemoteSnapshot(); r.dailyLogs = [androidDay(20_780, flow: "MEDIUM", updatedAt: 5_000)]
        XCTAssertEqual(SyncMerge.merge(r, into: &d), 1)
        let log = d.day(20_780)!.domain
        XCTAssertEqual(log.flow, .MEDIUM)
        XCTAssertEqual(log.symptoms, ["Cramps": .MODERATE, "Bloating": .MILD])
        XCTAssertEqual(log.moods, ["Calm", "Tired"])
        XCTAssertEqual(log.moodScore, 4)
        XCTAssertNil(log.voiceNoteKey)
    }

    func testLastWriteWins() {
        var d = TuulitData()
        d.upsertDay(DayRow(epochDay: 10, flow: "LIGHT", updatedAt: 9_000))
        var r = RemoteSnapshot(); r.dailyLogs = [androidDay(10, flow: "HEAVY", updatedAt: 5_000)]
        XCTAssertEqual(SyncMerge.merge(r, into: &d), 0)
        XCTAssertEqual(d.day(10)?.flow, "LIGHT")
        r.dailyLogs = [androidDay(10, flow: "HEAVY", updatedAt: 9_500, deleted: true)]
        SyncMerge.merge(r, into: &d)
        XCTAssertEqual(d.day(10)?.deleted, true)
    }

    func testCycleStartConflictNewerWinsAndLoserIsShifted() {
        var d = TuulitData()
        d.upsertCycle(CycleRow(id: "local", startEpochDay: 100, updatedAt: 1_000))
        var r = RemoteSnapshot()
        r.cycles = [RemoteDoc(id: "remote", fields: ["startEpochDay": NSNumber(value: 100), "endEpochDay": NSNull(),
                                                     "updatedAt": NSNumber(value: 2_000), "deleted": false])]
        SyncMerge.merge(r, into: &d)
        XCTAssertEqual(d.liveCycles.map(\.id), ["remote"])
        XCTAssertEqual(d.cycles["local"]?.startEpochDay, -101)
        XCTAssertEqual(d.cycles["local"]?.deleted, true)
    }

    func testOlderRemoteCycleDoesNotReplaceNewerLocal() {
        var d = TuulitData()
        d.upsertCycle(CycleRow(id: "local", startEpochDay: 100, updatedAt: 3_000))
        var r = RemoteSnapshot()
        r.cycles = [RemoteDoc(id: "remote", fields: ["startEpochDay": 100, "updatedAt": 2_000, "deleted": false])]
        SyncMerge.merge(r, into: &d)
        XCTAssertEqual(d.liveCycles.map(\.id), ["local"])
    }

    func testGamesTakeMaximumAndAchievementsUnion() {
        var d = TuulitData()
        d.games["calm_tap"] = GameScoreRow(gameId: "calm_tap", bestScore: 90, plays: 4, updatedAt: 10)
        d.achievements["FIRST_GAME"] = 5
        var r = RemoteSnapshot()
        r.games = [RemoteDoc(id: "calm_tap", fields: ["bestScore": 60, "plays": 9, "updatedAt": 20])]
        r.achievements = [RemoteDoc(id: "FIRST_QUIZ", fields: ["unlockedAt": 7]), RemoteDoc(id: "FIRST_GAME", fields: ["unlockedAt": 99])]
        SyncMerge.merge(r, into: &d)
        XCTAssertEqual(d.games["calm_tap"]?.bestScore, 90)
        XCTAssertEqual(d.games["calm_tap"]?.plays, 9)
        XCTAssertEqual(d.achievements, ["FIRST_GAME": 5, "FIRST_QUIZ": 7])
    }

    func testUntouchedSeedKitIsDroppedWhenCloudHasAKit() {
        let store = TuulitStore.inMemory()
        CarePlayRepository(store: store).seedLibraryIfEmpty()
        var r = RemoteSnapshot()
        r.comfort = [RemoteDoc(id: "android-random", fields: ["kind": "kit", "title": "Pads or tampons", "icon": "check", "checked": false,
                                                              "helpedCount": 0, "sessionId": NSNull(), "position": 0, "updatedAt": 50, "deleted": false])]
        store.write { SyncMerge.merge(r, into: &$0) }
        XCTAssertEqual(CarePlayRepository(store: store).comfortItems.map(\.id), ["android-random"])
    }

    func testPushWritesOnlyChangesAndUsesAndroidFieldNames() {
        var d = TuulitData()
        d.upsertDay(DayRow.from(DailyLog(epochDay: 7, flow: .HEAVY, symptoms: ["Cramps": .SEVERE]), now: 200))
        d.upsertCycle(CycleRow(id: "c", startEpochDay: 7, updatedAt: 50))
        let w = SyncMerge.pushWrites(d, since: 100)
        XCTAssertEqual(w.count, 1)
        XCTAssertEqual(w[0].collection, "dailyLogs")
        XCTAssertEqual(w[0].docId, "7")
        XCTAssertEqual(w[0].fields["flow"] as? String, "HEAVY")
        XCTAssertEqual(w[0].fields["symptoms"] as? String, "Cramps:3")
        XCTAssertTrue(w[0].fields["voiceNoteKey"] is NSNull)
        XCTAssertEqual(w[0].fields["epochDay"] as? Int, 7)
    }

    func testRoundTripThroughPushAndMergeIsLossless() {
        let store = TuulitStore.inMemory()
        let repo = TrackingRepository(store: store)
        repo.saveDailyLog(DailyLog(epochDay: 300, flow: .LIGHT, symptoms: ["Headache": .MILD], moods: ["Low"], moodScore: 2, energyLevel: 1, journalText: "tired"))
        repo.savePainLog(PainLog(id: "p", epochDay: 300, intensity: 7, location: "Lower back", painType: "Dull ache", durationHours: 3,
                                 medicationTaken: "heat", impactedWorkOrStudy: false, impactedExercise: true, impactedSleep: false, notes: "x"))
        let writes = store.read { SyncMerge.pushWrites($0, since: 0) }
        var snap = RemoteSnapshot()
        for w in writes {
            let doc = RemoteDoc(id: w.docId, fields: w.fields)
            switch w.collection {
            case "cycles": snap.cycles.append(doc)
            case "dailyLogs": snap.dailyLogs.append(doc)
            case "painLogs": snap.painLogs.append(doc)
            default: break
            }
        }
        var fresh = TuulitData()
        SyncMerge.merge(snap, into: &fresh)
        XCTAssertEqual(fresh.liveDays.map(\.domain), store.read { $0.liveDays.map(\.domain) })
        XCTAssertEqual(fresh.livePain.map(\.domain), store.read { $0.livePain.map(\.domain) })
        XCTAssertEqual(fresh.liveCycles.map(\.domain), store.read { $0.liveCycles.map(\.domain) })
    }

    func testSettingsSyncLastWriteWins() {
        var s = UserSettings()
        s.syncedSettingsUpdatedAt = 100
        XCTAssertFalse(s.applyRemote(["cycleLength": 31, "updatedAt": 50]))
        XCTAssertTrue(s.applyRemote(["cycleLength": NSNumber(value: 31), "regularity": "IRREGULAR", "bananaMode": "GYM", "updatedAt": NSNumber(value: 200)]))
        XCTAssertEqual(s.cycleLength, 31)
        XCTAssertEqual(s.regularity, .IRREGULAR)
        XCTAssertEqual(s.bananaMode, .GYM)
    }

    func testConnectStatusOnlyPermittedFields() {
        let p = CycleCalculator.calculatePrediction(todayEpochDay: 101, cycles: [CycleRecord(id: "c", startEpochDay: 100)], dailyLogs: [],
                                                    fallbackCycleLength: 28, fallbackPeriodDuration: 5, regularity: .REGULAR)
        let off = ConnectRules.statusFields(permissions: SharePermissions(), prediction: p, today: 101)
        XCTAssertTrue(off["periodStartedOn"] is NSNull && off["phase"] is NSNull && off["periodSoonOn"] is NSNull)
        let on = ConnectRules.statusFields(permissions: SharePermissions(periodStarted: true, phase: true), prediction: p, today: 101)
        XCTAssertEqual(on["periodStartedOn"] as? Int, 100)
        XCTAssertEqual(on["phase"] as? String, CyclePhase.MENSTRUAL.title)
        let code = ConnectRules.newInviteCode()
        XCTAssertNil(Validation.inviteCode(code))
        XCTAssertNil(code.rangeOfCharacter(from: CharacterSet(charactersIn: "IO01")))
    }
}
