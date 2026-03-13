package org.ripple.endpoint.detector.api

import org.ripple.endpoint.detector.api.model.EntryType
import org.ripple.endpoint.detector.api.model.TrafficEntry

/**
 * 流量入口检测器 SPI 接口
 * 
 * 所有框架检测器需实现此接口，通过扩展点注册到插件中。
 */
interface TrafficDetector {
    
    /**
     * 检测器支持的入口类型
     */
    val entryType: EntryType
    
    /**
     * 检测器名称
     */
    val name: String
    
    /**
     * 检测器描述
     */
    val description: String
    
    /**
     * 默认配置
     */
    val defaultConfig: DetectorConfig
    
    /**
     * 检测指定内容中的流量入口
     * 
     * @param sourceCode 源代码内容
     * @param filePath 文件路径
     * @param config 检测器配置
     * @return 检测到的流量入口列表
     */
    fun detect(
        sourceCode: String,
        filePath: String,
        config: DetectorConfig
    ): List<TrafficEntry>
    
    /**
     * 判断文件是否适用此检测器
     * 
     * @param filePath 文件路径
     * @param config 检测器配置
     * @return 是否适用
     */
    fun isApplicable(filePath: String, config: DetectorConfig): Boolean
}