import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { createRecall, getBatches, getInspectionJobs, getLineage, getRecalls } from '../../api';
import { normalizeApiError } from '../../lib/apiErrors';
import { queryKeys } from '../../lib/queryKeys';

export default function OperatorConsole() {
  const [organizationId, setOrganizationId] = useState('');
  const [batchId, setBatchId] = useState('');
  const [recallReason, setRecallReason] = useState('Suspected contamination');
  const queryClient = useQueryClient();

  const batches = useQuery({
    queryKey: queryKeys.batches(organizationId),
    queryFn: () => getBatches(organizationId),
    enabled: organizationId.length > 0,
  });
  const inspections = useQuery({
    queryKey: queryKeys.inspectionJobs(organizationId),
    queryFn: () => getInspectionJobs(organizationId),
    enabled: organizationId.length > 0,
  });
  const recalls = useQuery({
    queryKey: queryKeys.recalls(organizationId),
    queryFn: () => getRecalls(organizationId),
    enabled: organizationId.length > 0,
  });
  const lineage = useQuery({
    queryKey: queryKeys.lineage(batchId),
    queryFn: () => getLineage(batchId),
    enabled: batchId.length > 0,
  });

  const recallSimulation = useMutation({
    mutationFn: () => createRecall({ sourceBatchId: batchId, reason: recallReason, simulation: true }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.recalls(organizationId) }),
  });

  const errorMessage = useMemo(() => {
    const error = batches.error || inspections.error || recalls.error || lineage.error || recallSimulation.error;
    return error ? normalizeApiError(error).message : '';
  }, [batches.error, inspections.error, recalls.error, lineage.error, recallSimulation.error]);

  return (
    <section className="space-y-6">
      <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-6">
        <div className="grid gap-4 md:grid-cols-[1fr_1fr_auto]">
          <label className="block">
            <span className="mb-1 block text-xs font-medium text-slate-400">Organization ID</span>
            <input
              value={organizationId}
              onChange={(event) => setOrganizationId(event.target.value.trim())}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-medium text-slate-400">Source Batch ID</span>
            <input
              value={batchId}
              onChange={(event) => setBatchId(event.target.value.trim())}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none"
            />
          </label>
          <button
            type="button"
            disabled={!batchId || recallSimulation.isPending}
            onClick={() => recallSimulation.mutate()}
            className="self-end rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-emerald-950 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Simulate Recall
          </button>
        </div>
        <label className="mt-4 block">
          <span className="mb-1 block text-xs font-medium text-slate-400">Recall Reason</span>
          <input
            value={recallReason}
            onChange={(event) => setRecallReason(event.target.value)}
            className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus:border-emerald-500 focus:outline-none"
          />
        </label>
        {errorMessage && <p className="mt-3 text-sm text-red-300">{errorMessage}</p>}
      </div>

      <div className="grid gap-4 lg:grid-cols-4">
        <Metric label="Batches" value={batches.data?.length ?? 0} loading={batches.isFetching} />
        <Metric label="Inspections" value={inspections.data?.length ?? 0} loading={inspections.isFetching} />
        <Metric label="Lineage Edges" value={lineage.data?.length ?? 0} loading={lineage.isFetching} />
        <Metric label="Recall Cases" value={recalls.data?.length ?? 0} loading={recalls.isFetching} />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Panel title="Inspection Review Queue">
          {(inspections.data ?? []).slice(0, 5).map((job) => (
            <Row key={job.id} title={job.id} detail={`${job.status} / ${job.finalDecision ?? job.automatedDecision ?? 'PENDING'}`} />
          ))}
          {organizationId && inspections.data?.length === 0 && <Empty />}
        </Panel>
        <Panel title="Recall Scope">
          {(recalls.data ?? []).slice(0, 5).map((recall) => (
            <Row key={recall.id} title={recall.sourceBatchId} detail={`${recall.status} / ${recall.scope?.affectedBatchIds?.length ?? 0} batches`} />
          ))}
          {organizationId && recalls.data?.length === 0 && <Empty />}
        </Panel>
      </div>
    </section>
  );
}

function Metric({ label, value, loading }: { label: string; value: number; loading: boolean }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
      <p className="text-xs font-medium uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-2 text-2xl font-bold text-slate-100">{loading ? '...' : value}</p>
    </div>
  );
}

function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-900/60 p-4">
      <h3 className="text-sm font-semibold text-slate-200">{title}</h3>
      <div className="mt-4 space-y-2">{children}</div>
    </div>
  );
}

function Row({ title, detail }: { title: string; detail: string }) {
  return (
    <div className="rounded-md border border-slate-800 bg-slate-950 px-3 py-2">
      <p className="text-sm font-medium text-slate-200">{title}</p>
      <p className="text-xs text-slate-500">{detail}</p>
    </div>
  );
}

function Empty() {
  return <p className="rounded-md border border-dashed border-slate-800 px-3 py-6 text-center text-sm text-slate-500">No records found.</p>;
}
