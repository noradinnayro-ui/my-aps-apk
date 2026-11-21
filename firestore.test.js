const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any user profile, config, or focus session", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("configs").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("focus_sessions").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      completedFocusSessions: 0,
      totalFocusMinutes: 0,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Authenticated owner: UserProfile create, partial update, and immutable field guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Dev",
      email: "alice@example.com",
      completedFocusSessions: 1,
      totalFocusMinutes: 25,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update (mutable fields only)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      completedFocusSessions: 2,
      totalFocusMinutes: 50,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      completedFocusSessions: 3,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Authenticated owner: SavedConfig create, update, list, delete and ghost-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);

  // Empty collection list query must succeed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("configs").get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("configs").doc("cfg_1").set({
      userId: ALICE_UID,
      title: "settings_dd06cfbb.json",
      jsonContent: "{\"files.autoSave\":\"onFocusChange\"}",
      keysCount: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("configs").doc("cfg_1").update({
      title: "Zen Settings",
      jsonContent: "{\"files.autoSave\":\"afterDelay\"}",
      keysCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId `id` field or ghost fields
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("configs").doc("cfg_2").set({
      id: "cfg_2",
      userId: ALICE_UID,
      title: "Bad Config",
      jsonContent: "{}",
      keysCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("configs").doc("cfg_3").set({
      userId: ALICE_UID,
      title: "Ghost Config",
      jsonContent: "{}",
      keysCount: 0,
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("configs").doc("cfg_1").delete()
  );
});

test("Authenticated owner: FocusSessionLog create & immutable update rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("focus_sessions").doc("sess_1").set({
      userId: ALICE_UID,
      presetName: "CLASSIC",
      durationMinutes: 25,
      createdAt: created,
    })
  );

  // Immutable log entries cannot be updated
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("focus_sessions").doc("sess_1").update({
      durationMinutes: 90,
    })
  );

  // Out-of-bounds duration rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("focus_sessions").doc("sess_2").set({
      userId: ALICE_UID,
      presetName: "CLASSIC",
      durationMinutes: 500,
      createdAt: created,
    })
  );
});

test("Cross-user isolation: Alice cannot read or modify Bob's profile, configs, or focus sessions", async () => {
  const past = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      completedFocusSessions: 4,
      totalFocusMinutes: 100,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("users").doc(BOB_UID).collection("configs").doc("bob_cfg").set({
      userId: BOB_UID,
      title: "Bob Config",
      jsonContent: "{}",
      keysCount: 0,
      createdAt: past,
      updatedAt: past,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("configs").get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("configs").doc("bob_cfg").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("configs").doc("bob_cfg").delete()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("active_timer").doc("current").get()
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).collection("active_timer").doc("current").set({
      userId: BOB_UID,
      taskName: "Hijack",
      presetName: "CLASSIC",
      phase: "FOCUS",
      totalSeconds: 1500,
      remainingSeconds: 900,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: ActiveTimerState create, partial update, validation & unauth denial", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("active_timer").doc("current").get()
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_timer").doc("current").set({
      userId: ALICE_UID,
      taskName: "Refactor Auth Flow",
      presetName: "CLASSIC",
      phase: "FOCUS",
      totalSeconds: 1500,
      remainingSeconds: 1200,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update preserving userId and createdAt
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_timer").doc("current").update({
      taskName: "Ship Release",
      remainingSeconds: 850,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject remainingSeconds > totalSeconds
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_timer").doc("current").update({
      remainingSeconds: 5000,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject invalid phase enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_timer").doc("current").update({
      phase: "INVALID_PHASE",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject @DocumentId `id` ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_timer").doc("other").set({
      id: "other",
      userId: ALICE_UID,
      taskName: "Bad",
      presetName: "CLASSIC",
      phase: "FOCUS",
      totalSeconds: 1500,
      remainingSeconds: 1500,
      createdAt: created,
      updatedAt: created,
    })
  );
});
