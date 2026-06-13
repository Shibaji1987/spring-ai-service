/*
 * Idempotent MongoDB bootstrap for the Agentic AI Audit Analysis Service.
 *
 * Run with:
 *   mongosh "mongodb://localhost:27017" --file scripts/initialize-mongodb.js
 *
 * Optional environment variable:
 *   MONGODB_DATABASE=audit_platform
 *
 * This script intentionally does not create a vector index. Policy embeddings
 * are stored as ordinary arrays in knowledge_chunks and cosine similarity is
 * calculated by the Java application.
 */

const databaseName = process.env.MONGODB_DATABASE || "audit_platform";
const auditDb = db.getSiblingDB(databaseName);

const collectionDefinitions = [
  {
    name: "audit_events",
    indexes: [
      { key: { actor: 1 }, name: "idx_audit_events_actor" },
      { key: { status: 1 }, name: "idx_audit_events_status" },
      { key: { eventType: 1 }, name: "idx_audit_events_type" },
      { key: { eventTime: -1 }, name: "idx_audit_events_time_desc" },
      {
        key: { actor: 1, eventTime: -1 },
        name: "idx_audit_events_actor_time_desc"
      },
      {
        key: { target: 1, eventTime: -1 },
        name: "idx_audit_events_target_time_desc"
      }
    ]
  },
  {
    name: "audit_ai_analysis",
    indexes: [
      {
        key: { eventId: 1 },
        name: "uq_audit_analysis_event",
        unique: true
      },
      { key: { category: 1 }, name: "idx_audit_analysis_category" },
      { key: { analyzedAt: -1 }, name: "idx_audit_analysis_time_desc" },
      {
        key: { riskScore: -1, analyzedAt: -1 },
        name: "idx_audit_analysis_risk_time"
      },
      {
        key: { grounded: 1, analyzedAt: -1 },
        name: "idx_audit_analysis_grounded_time"
      }
    ]
  },
  {
    name: "audit_analysis_runs",
    indexes: [
      {
        key: { eventId: 1, createdAt: -1 },
        name: "idx_analysis_runs_event_created"
      },
      {
        key: { status: 1, createdAt: -1 },
        name: "idx_analysis_runs_status_created"
      }
    ]
  },
  {
    name: "knowledge_documents",
    indexes: [
      { key: { createdAt: -1 }, name: "idx_knowledge_documents_created" },
      {
        key: { sourceType: 1, createdAt: -1 },
        name: "idx_knowledge_documents_source_created"
      },
      { key: { title: 1 }, name: "idx_knowledge_documents_title" },
      { key: { tags: 1 }, name: "idx_knowledge_documents_tags" }
    ]
  },
  {
    name: "knowledge_chunks",
    indexes: [
      {
        key: { documentId: 1, chunkIndex: 1 },
        name: "uq_knowledge_chunks_document_position",
        unique: true
      },
      {
        key: { documentTitle: 1 },
        name: "idx_knowledge_chunks_document_title"
      },
      { key: { createdAt: -1 }, name: "idx_knowledge_chunks_created" }
    ]
  }
];

function ensureCollection(definition) {
  const exists = auditDb.getCollectionNames().includes(definition.name);
  if (!exists) {
    auditDb.createCollection(definition.name);
    print(`Created collection: ${definition.name}`);
  } else {
    print(`Collection already exists: ${definition.name}`);
  }

  const collection = auditDb.getCollection(definition.name);
  for (const index of definition.indexes) {
    const options = { name: index.name };
    if (index.unique) {
      options.unique = true;
    }

    collection.createIndex(index.key, options);
    print(`Ensured index: ${definition.name}.${index.name}`);
  }
}

print(`Initializing MongoDB database: ${databaseName}`);
for (const definition of collectionDefinitions) {
  ensureCollection(definition);
}

print("");
print("MongoDB initialization completed.");
print("Collections:");
for (const definition of collectionDefinitions) {
  const count = auditDb.getCollection(definition.name).countDocuments({});
  print(`- ${definition.name}: ${count} document(s)`);
}
print("");
print("No vector index was created. RAG retrieval currently uses Java-side cosine similarity.");
