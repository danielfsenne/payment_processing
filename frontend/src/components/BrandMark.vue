<script setup lang="ts">
withDefaults(defineProps<{ size?: 'sm' | 'md' | 'lg' }>(), { size: 'md' })
</script>

<template>
  <div class="flex items-center gap-2.5">
    <span
      class="flex shrink-0 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-600 font-bold text-white shadow-sm shadow-indigo-500/30"
      :class="{
        'h-7 w-7 text-xs': size === 'sm',
        'h-9 w-9 text-sm': size === 'md',
        'h-12 w-12 text-lg': size === 'lg',
      }"
    >
      P
    </span>
    <span
      class="font-bold tracking-tight text-slate-900"
      :class="{ 'text-sm': size === 'sm', 'text-base': size === 'md', 'text-xl': size === 'lg' }"
    >
      Payment Processing
    </span>
  </div>
</template>
