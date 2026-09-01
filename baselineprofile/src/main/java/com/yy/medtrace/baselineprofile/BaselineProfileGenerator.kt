package com.yy.medtrace.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 生成医迹的 Baseline Profile。
 *
 * 在已连接真机/模拟器（Android 9+）上执行：
 *     ./gradlew :baselineprofile:connectedBenchmarkAndroidTest
 * 运行结束后会在 :baselineprofile/build/outputs/ 下产出 baseline-prof.txt，
 * 将其内容拷贝/覆盖到 app/src/main/baseline-prof.txt 即可。release 包会据此在
 * 装包时对启动热路径做 AOT 编译，显著缩短冷启动时间。
 *
 * 仅采集启动路径即可覆盖绝大多数冷启动开销；如需更优，可在 collect 闭包内
 * 追加交互（如滚动首页、进入记录页）。
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() {
        baselineProfileRule.collect(packageName = "com.yy.medtrace") {
            // 启动医迹并等待首帧稳定
            startActivityAndWait()
            device.waitForIdle()
        }
    }
}
