<script setup lang="ts">
import { Collection, FolderAdd, Files, Setting, Tickets } from '@element-plus/icons-vue';

const navItems = [
  {
    name: 'capture',
    label: '收纳',
    description: '路径与上传入口',
    icon: FolderAdd
  },
  {
    name: 'inbox',
    label: '资料箱',
    description: '原始文件账本',
    icon: Files
  },
  {
    name: 'wiki',
    label: 'Wiki',
    description: 'Obsidian 整理层',
    icon: Collection
  },
  {
    name: 'logs',
    label: '日志',
    description: '任务和写入记录',
    icon: Tickets
  },
  {
    name: 'settings',
    label: '设置',
    description: '路径、安全、字典',
    icon: Setting
  }
] as const;
</script>

<template>
  <aside class="sidebar-nav">
    <div class="sidebar-brand">
      <div class="brand-mark">WF</div>
      <div>
        <strong>WikiForge</strong>
        <span>MVP0 私有知识库</span>
      </div>
    </div>

    <nav class="nav-group" aria-label="MVP0 主导航">
      <p class="nav-group-title">MVP0 主流程</p>
      <RouterLink
        v-for="item in navItems"
        :key="item.name"
        v-slot="{ isActive, navigate }"
        :to="{ name: item.name }"
        custom
      >
        <button
          class="nav-button"
          :class="{ 'is-active': isActive }"
          type="button"
          @click="navigate"
        >
          <span class="nav-dot" />
          <span class="nav-button-content">
            <strong>
              <el-icon><component :is="item.icon" /></el-icon>
              {{ item.label }}
            </strong>
            <small>{{ item.description }}</small>
          </span>
        </button>
      </RouterLink>
    </nav>
  </aside>
</template>
