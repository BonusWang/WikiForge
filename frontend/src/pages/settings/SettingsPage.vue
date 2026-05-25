<script setup lang="ts">
import { Setting } from '@element-plus/icons-vue';
</script>

<template>
  <div class="mvp0-stack">
    <el-card shadow="never">
      <template #header>
        <div class="section-title">
          <div class="card-title">
            <el-icon><Setting /></el-icon>
            MVP0 运行设置
          </div>
          <el-tag effect="plain">强约定</el-tag>
        </div>
      </template>

      <div class="settings-grid">
        <div class="setting-row">
          <span>主入口</span>
          <strong>收纳 / 资料箱 / Wiki / 日志 / 设置</strong>
        </div>
        <div class="setting-row">
          <span>状态码</span>
          <strong>中文字典映射</strong>
        </div>
        <div class="setting-row">
          <span>资料仓库</span>
          <strong>WikiForge/30_Resources_资源</strong>
        </div>
        <div class="setting-row">
          <span>Obsidian</span>
          <strong>Vault 内 WikiForge 托管目录</strong>
        </div>
      </div>
    </el-card>
  </div>
</template>
