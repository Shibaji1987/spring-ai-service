import { createHash } from 'node:crypto';

const args = new Map(
  process.argv.slice(2).map((argument) => {
    const [key, ...value] = argument.split('=');
    return [key.replace(/^--/, ''), value.join('=')];
  })
);

const baseUrl = args.get('base-url') ?? 'http://localhost:8080';
const username = args.get('username') ?? process.env.APP_SECURITY_USERNAME ?? 'analyst';
const password = args.get('password') ?? process.env.APP_SECURITY_PASSWORD ?? 'Analyst@12345';
const concurrency = Number(args.get('concurrency') ?? 4);
const dryRun = args.has('dry-run');
const batchId = 'enterprise-policy-corpus-v1';

const domains = [
  { code: 'IAM', name: 'Identity and Access Management', owner: 'Identity Security', system: 'workforce identity platform' },
  { code: 'PAM', name: 'Privileged Access Management', owner: 'Privileged Access Office', system: 'privileged access vault' },
  { code: 'DAT', name: 'Data Protection', owner: 'Data Security', system: 'enterprise data lake' },
  { code: 'NET', name: 'Network Security', owner: 'Network Defense', system: 'zero trust network fabric' },
  { code: 'CLD', name: 'Cloud Security', owner: 'Cloud Security Engineering', system: 'multi-cloud control plane' },
  { code: 'PAY', name: 'Payment Security', owner: 'Payment Risk', system: 'real-time payment gateway' },
  { code: 'CUS', name: 'Customer Privacy', owner: 'Privacy Office', system: 'customer profile platform' },
  { code: 'OPS', name: 'Production Operations', owner: 'Site Reliability Engineering', system: 'production orchestration platform' },
  { code: 'API', name: 'API and Service Security', owner: 'Application Security', system: 'enterprise API gateway' },
  { code: 'TPR', name: 'Third-Party Risk', owner: 'Vendor Risk Management', system: 'partner integration hub' }
];

const actions = [
  { code: 'AUTH', name: 'authentication and session establishment', verb: 'authenticate to', signal: 'failed authentication attempts' },
  { code: 'PRIV', name: 'privilege elevation', verb: 'elevate privileges within', signal: 'unexpected role elevation' },
  { code: 'READ', name: 'sensitive record access', verb: 'read sensitive records from', signal: 'high-volume record access' },
  { code: 'EXPT', name: 'bulk data export', verb: 'export regulated data from', signal: 'bulk export volume' },
  { code: 'CONF', name: 'security configuration change', verb: 'change security configuration in', signal: 'control-plane configuration drift' },
  { code: 'KEYS', name: 'cryptographic key use', verb: 'use cryptographic keys in', signal: 'unusual key invocation' },
  { code: 'DEPL', name: 'production deployment', verb: 'deploy software to', signal: 'unapproved production deployment' },
  { code: 'DELT', name: 'record deletion', verb: 'delete protected records from', signal: 'destructive record activity' },
  { code: 'TOKN', name: 'service token issuance', verb: 'issue service tokens through', signal: 'abnormal token issuance' },
  { code: 'SHAR', name: 'external data sharing', verb: 'share protected information through', signal: 'new external recipient activity' }
];

const scenarios = [
  { code: 'STD', name: 'standard business operations', approval: 'line manager and system owner', window: 'approved business hours', severity: 'MEDIUM' },
  { code: 'ELEV', name: 'elevated-risk operations', approval: 'system owner and security duty officer', window: 'a four-hour approved change window', severity: 'HIGH' },
  { code: 'BRK', name: 'break-glass emergency access', approval: 'incident commander with retrospective control-owner review', window: 'a sixty-minute emergency session', severity: 'CRITICAL' },
  { code: 'VEND', name: 'third-party maintenance', approval: 'vendor manager, system owner, and security operations', window: 'a supervised maintenance window', severity: 'HIGH' },
  { code: 'AUTO', name: 'automated service execution', approval: 'service owner through a reviewed machine identity', window: 'the registered workload schedule', severity: 'MEDIUM' }
];

function policyFor(domain, action, scenario, index) {
  const sequence = String(index + 1).padStart(3, '0');
  const controlId = `${domain.code}-${action.code}-${scenario.code}-${sequence}`;
  const threshold = 3 + ((index * 7) % 48);
  const reviewMinutes = 15 + ((index * 11) % 106);
  const retentionDays = 180 + ((index * 13) % 916);
  const confidence = 70 + ((index * 3) % 29);
  const title = `${controlId} ${domain.name}: ${action.name} - ${scenario.name}`;
  const content = [
    `Control ${controlId}. ${title}.`,
    `Scope: all human and machine identities that ${action.verb} the ${domain.system} during ${scenario.name}.`,
    `Authorization: ${scenario.approval} must approve the activity, which is restricted to ${scenario.window}.`,
    `Preventive controls: require phishing-resistant MFA or workload identity, least privilege, managed-device posture, purpose binding, and a ticket reference.`,
    `Detection: alert when ${action.signal} reaches ${threshold} events in 15 minutes, when behavioral confidence exceeds ${confidence} percent, or when geography, device, recipient, or workload identity is untrusted.`,
    `Response: classify the event as ${scenario.severity}, preserve evidence for ${retentionDays} days, notify ${domain.owner}, and begin triage within ${reviewMinutes} minutes.`,
    `Exceptions require a named risk owner, compensating controls, an expiry time, and post-activity review. Evidence must include actor, target, source, approval, session, request, outcome, and correlation identifiers.`
  ].join(' ');

  return {
    title,
    sourceType: 'POLICY',
    content,
    tags: [
      'synthetic-enterprise-policy',
      batchId,
      domain.code.toLowerCase(),
      action.code.toLowerCase(),
      scenario.code.toLowerCase(),
      scenario.severity.toLowerCase()
    ],
    metadata: {
      batchId,
      controlId,
      domain: domain.name,
      action: action.name,
      scenario: scenario.name,
      owner: domain.owner,
      targetSystem: domain.system,
      severity: scenario.severity,
      threshold,
      reviewMinutes,
      retentionDays,
      behavioralConfidencePercent: confidence,
      synthetic: true,
      version: 1
    }
  };
}

const policies = [];
for (const domain of domains) {
  for (const action of actions) {
    for (const scenario of scenarios) {
      policies.push(policyFor(domain, action, scenario, policies.length));
    }
  }
}

function assertUnique(values, label) {
  if (new Set(values).size !== values.length) {
    throw new Error(`${label} values are not unique.`);
  }
}

assertUnique(policies.map((policy) => policy.title), 'Policy title');
assertUnique(policies.map((policy) => policy.metadata.controlId), 'Control ID');
assertUnique(
  policies.map((policy) => createHash('sha256').update(policy.content).digest('hex')),
  'Policy content'
);

if (policies.length !== 500) {
  throw new Error(`Expected 500 policies but generated ${policies.length}.`);
}

console.log(`Generated ${policies.length} unique policies for batch ${batchId}.`);
console.log(`First control: ${policies[0].metadata.controlId}`);
console.log(`Last control: ${policies.at(-1).metadata.controlId}`);

if (dryRun) {
  console.log('Dry run complete. No policies were ingested.');
  process.exit(0);
}

const loginResponse = await fetch(`${baseUrl}/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});

if (!loginResponse.ok) {
  throw new Error(`Login failed with HTTP ${loginResponse.status}.`);
}

const { accessToken } = await loginResponse.json();
let nextIndex = 0;
let completed = 0;
const failures = [];
const startedAt = Date.now();

async function ingest(policy, index) {
  for (let attempt = 1; attempt <= 4; attempt++) {
    try {
      const response = await fetch(`${baseUrl}/knowledge/documents`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(policy)
      });

      if (response.ok) {
        return;
      }

      const responseBody = await response.text();
      if (response.status < 500 && response.status !== 429) {
        throw new Error(`HTTP ${response.status}: ${responseBody}`);
      }
      throw new Error(`retryable HTTP ${response.status}: ${responseBody}`);
    } catch (error) {
      if (attempt === 4) {
        throw error;
      }
      await new Promise((resolve) => setTimeout(resolve, 500 * (2 ** (attempt - 1))));
    }
  }

  throw new Error(`Policy ${index + 1} failed without an error.`);
}

async function worker() {
  while (true) {
    const index = nextIndex++;
    if (index >= policies.length) {
      return;
    }

    try {
      await ingest(policies[index], index);
      completed++;
      if (completed % 25 === 0 || completed === policies.length) {
        const seconds = ((Date.now() - startedAt) / 1000).toFixed(1);
        console.log(`Ingested ${completed}/${policies.length} policies in ${seconds}s.`);
      }
    } catch (error) {
      failures.push({
        controlId: policies[index].metadata.controlId,
        error: error instanceof Error ? error.message : String(error)
      });
    }
  }
}

await Promise.all(Array.from({ length: concurrency }, () => worker()));

const elapsedSeconds = ((Date.now() - startedAt) / 1000).toFixed(1);
console.log(`Completed batch in ${elapsedSeconds}s: ${completed} succeeded, ${failures.length} failed.`);

if (failures.length > 0) {
  console.error(JSON.stringify(failures, null, 2));
  process.exitCode = 1;
}
