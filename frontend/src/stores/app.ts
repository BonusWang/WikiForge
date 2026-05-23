import { defineStore } from 'pinia';

export const useAppStore = defineStore('app', {
  state: () => ({
    appName: '知识熔炉 WikiForge',
    stage: 'V1 LifeOS'
  })
});
