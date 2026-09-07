<script setup lang="ts">
import { computed } from 'vue'
import type { PaymentStatus } from '@/types'

const props = defineProps<{ status: PaymentStatus }>()

const colors: Record<PaymentStatus, string> = {
  CREATED: 'bg-gray-100 text-gray-700',
  PROCESSING: 'bg-yellow-100 text-yellow-700',
  AUTHORIZED: 'bg-yellow-100 text-yellow-700',
  CAPTURED: 'bg-blue-100 text-blue-700',
  SETTLED: 'bg-green-100 text-green-700',
  FAILED: 'bg-red-100 text-red-700',
  RETRYING: 'bg-orange-100 text-orange-700',
}

const classes = computed(() => colors[props.status] ?? 'bg-gray-100 text-gray-700')
</script>

<template>
  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="classes">{{ status }}</span>
</template>
