import { computed, type ComputedRef } from 'vue'
import {
  TaskStatusMap,
  PrescriptionStatusMap,
  DeliveryStatusMap,
  DeviceBizStatusMap,
  getStatusLabel,
  getStatusType,
} from '@/constants/statusMap'

/**
 * 状态标签统一获取 Composable
 *
 * 原则：禁止在模板和逻辑中裸写中文字符串
 */
export function useStatus() {
  /**
   * 获取任务状态标签
   */
  const getTaskStatusLabel = (status: string | undefined): ComputedRef<string> => {
    return computed(() => getStatusLabel(status, TaskStatusMap))
  }

  /**
   * 获取处方接收状态标签
   */
  const getPrescriptionStatusLabel = (status: string | undefined): ComputedRef<string> => {
    return computed(() => getStatusLabel(status, PrescriptionStatusMap))
  }

  /**
   * 获取发药状态标签
   */
  const getDeliveryStatusLabel = (status: string | undefined): ComputedRef<string> => {
    return computed(() => getStatusLabel(status, DeliveryStatusMap))
  }

  /**
   * 获取设备业务状态标签
   */
  const getDeviceStatusLabel = (status: string | undefined): ComputedRef<string> => {
    return computed(() => getStatusLabel(status, DeviceBizStatusMap))
  }

  /**
   * 获取状态标签类型（用于 el-tag）
   */
  const getTaskStatusType = (status: string | undefined): ComputedRef<'success' | 'warning' | 'danger' | 'info'> => {
    return computed(() => getStatusType(status))
  }

  return {
    getTaskStatusLabel,
    getPrescriptionStatusLabel,
    getDeliveryStatusLabel,
    getDeviceStatusLabel,
    getTaskStatusType,
  }
}
