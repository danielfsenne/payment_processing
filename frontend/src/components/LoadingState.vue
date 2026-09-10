<script setup lang="ts">
withDefaults(defineProps<{ label?: string }>(), { label: 'Carregando...' })
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-3 py-16 text-slate-400">
    <svg class="h-6 w-6 animate-spin text-indigo-500" viewBox="0 0 24 24" fill="none">
      <circle class="opacity-20" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
      <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
    </svg>
    <p class="text-sm">{{ label }}</p>
  </div>
</template>
