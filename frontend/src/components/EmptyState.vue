<script setup lang="ts">
withDefaults(defineProps<{ message?: string }>(), { message: 'Nada por aqui ainda.' })
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-3 rounded-2xl border border-dashed border-slate-200 bg-slate-50/60 py-16 text-center">
    <span class="flex h-11 w-11 items-center justify-center rounded-full bg-slate-100 text-slate-400">
      <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
        <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9.75h16.5M3.75 9.75v9a1.5 1.5 0 0 0 1.5 1.5h13.5a1.5 1.5 0 0 0 1.5-1.5v-9M3.75 9.75l1.83-4.58a1.5 1.5 0 0 1 1.393-.92h9.554a1.5 1.5 0 0 1 1.393.92l1.83 4.58" />
      </svg>
    </span>
    <p class="text-sm text-slate-500">{{ message }}</p>
  </div>
</template>
