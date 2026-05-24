import { createRouter, createWebHistory } from 'vue-router';
import CapturePage from '../pages/capture/CapturePage.vue';
import SourceInboxPage from '../pages/inbox/SourceInboxPage.vue';
import RunLogPage from '../pages/logs/RunLogPage.vue';
import SettingsPage from '../pages/settings/SettingsPage.vue';
import WikiWorkspacePage from '../pages/wiki/WikiWorkspacePage.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: { name: 'capture' }
    },
    {
      path: '/capture',
      name: 'capture',
      component: CapturePage,
      meta: {
        title: '收纳',
        subtitle: '统一入口接收本地路径和浏览器上传，先把原始文件规整收纳。'
      }
    },
    {
      path: '/inbox',
      name: 'inbox',
      component: SourceInboxPage,
      meta: {
        title: '资料箱',
        subtitle: '查看 Raw Sources 账本、收纳任务和正文抽取状态。'
      }
    },
    {
      path: '/wiki',
      name: 'wiki',
      component: WikiWorkspacePage,
      meta: {
        title: 'Wiki',
        subtitle: '面向 Obsidian 的 LLM Wiki 自动整理层。'
      }
    },
    {
      path: '/logs',
      name: 'logs',
      component: RunLogPage,
      meta: {
        title: '日志',
        subtitle: '集中查看收纳任务、写入运行和失败原因。'
      }
    },
    {
      path: '/settings',
      name: 'settings',
      component: SettingsPage,
      meta: {
        title: '设置',
        subtitle: '维护路径、安全边界和中文状态字典。'
      }
    }
  ]
});

export default router;
