<script setup lang="ts">
import { computed } from 'vue'
import type { PaymentStatus } from '@/types'

const props = defineProps<{ status: PaymentStatus }>()

const styles: Record<PaymentStatus, { badge: string; dot: string }> = {
  CREATED: { badge: 'bg-slate-100 text-slate-600', dot: 'bg-slate-400' },
  PROCESSING: { badge: 'bg-amber-100 text-amber-700', dot: 'bg-amber-500' },
  AUTHORIZED: { badge: 'bg-amber-100 text-amber-700', dot: 'bg-amber-500' },
  CAPTURED: { badge: 'bg-indigo-100 text-indigo-700', dot: 'bg-indigo-500' },
  SETTLED: { badge: 'bg-emerald-100 text-emerald-700', dot: 'bg-emerald-500' },
  FAILED: { badge: 'bg-rose-100 text-rose-700', dot: 'bg-rose-500' },
  RETRYING: { badge: 'bg-orange-100 text-orange-700', dot: 'bg-orange-500' },
}

const style = computed(() => styles[props.status] ?? styles.CREATED)

const labels: Record<PaymentStatus, string> = {
  CREATED: 'Criado',
  PROCESSING: 'Processando',
  AUTHORIZED: 'Autorizado',
  CAPTURED: 'Capturado',
  SETTLED: 'Liquidado',
  FAILED: 'Falhou',
  RETRYING: 'Retentando',
}
</script>

<template>
  <span
    class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-semibold"
    :class="style.badge"
  >
    <span class="h-1.5 w-1.5 rounded-full" :class="style.dot"></span>
    {{ labels[status] ?? status }}
  </span>
</template>
