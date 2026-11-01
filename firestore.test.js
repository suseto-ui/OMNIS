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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      host: emulatorHost,
      port: emulatorPort,
      rules: fs.readFileSync("firestore.rules", "utf8"),
    },
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("Unauthenticated user cannot read user profile", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Alice can create her own profile with valid data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      displayName: "Alice",
      createdAt: new Date(),
    })
  );
});

test("Alice cannot create Bob's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      createdAt: new Date(),
    })
  );
});

test("Alice can create and read her own record", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const recordDoc = aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1");
  await assertSucceeds(
    recordDoc.set({
      id: "rec_1",
      userId: ALICE_UID,
      query: "Analyze systemic stability",
      response: "System stable with 98% confidence",
      domain: "SYSTEMS",
      confidenceScore: 0.98,
      createdAt: new Date(),
    })
  );
  await assertSucceeds(recordDoc.get());
});

test("Bob cannot read Alice's record", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  
  await aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").set({
    id: "rec_1",
    userId: ALICE_UID,
    query: "Private query",
    response: "Private response",
    createdAt: new Date(),
  });

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_1").get()
  );
});

test("Alice cannot inject invalid fields or schema into records", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("records").doc("rec_invalid").set({
      id: "rec_invalid",
      userId: ALICE_UID,
      query: "Invalid payload",
      // missing response
      createdAt: new Date(),
      ghostField: "malicious_injection",
    })
  );
});
