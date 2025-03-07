package com.yy.chiyaole.data.model

/**
 * 服药状态枚举类
 *
 * @property PENDING 待服用：提醒已发出但用户尚未操作
 * @property TAKEN 已服用：用户已确认服用药物
 * @property SKIPPED 已跳过：用户主动选择跳过服用
 * @property DELAYED 已延迟：用户选择延迟服用，等待新的提醒
 * @property MISSED 已错过：超过服药时间且用户未进行任何操作
 */
enum class MedicationStatus {
    PENDING,    // 待服用：提醒已发出但用户尚未操作
    TAKEN,      // 已服用：用户已确认服用药物
    SKIPPED,    // 已跳过：用户主动选择跳过服用
    DELAYED,    // 已延迟：用户选择延迟服用，等待新的提醒
    MISSED      // 已错过：超过服药时间且用户未进行任何操作
}
