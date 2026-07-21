# 性能优化阶段1设计文档

## 概述

本阶段专注于修复MedTrace应用中的性能问题，主要解决主线程阻塞、数据库查询效率和内存使用问题。

## 目标

1. 修复主线程位图解码导致的卡顿
2. 优化N+1查询模式，提高数据库查询效率
3. 添加分页支持，避免加载过多数据
4. 优化HTTP客户端使用，提高网络请求效率

## 具体优化项

### 1. 主线程位图解码优化

**问题**：`MemberAvatar.kt`中在`remember`块内直接调用`BitmapFactory.decodeFile()`，这是一个同步磁盘I/O操作，会在组合期间运行，导致UI卡顿。

**解决方案**：
- 使用`Dispatchers.IO`在后台线程解码位图
- 使用`MutableStateFlow`或`mutableStateOf`管理位图状态
- 添加加载状态指示器

**涉及文件**：
- `app/src/main/java/com/yy/medtrace/ui/components/MemberAvatar.kt`

### 2. N+1查询模式优化

**问题**：`FamilyScreen.kt`中对每个成员都调用`countByMember()`，导致N+1查询问题。

**解决方案**：
- 在DAO中添加批量查询方法，一次性获取所有成员的记录数量
- 使用`GROUP BY`查询替代循环单个查询
- 在Repository层添加缓存机制

**涉及文件**：
- `app/src/main/java/com/yy/medtrace/data/dao/MedicalRecordDao.kt`
- `app/src/main/java/com/yy/medtrace/data/repository/RecordRepository.kt`
- `app/src/main/java/com/yy/medtrace/ui/screens/FamilyScreen.kt`

### 3. 分页支持

**问题**：`MedicalRecordScreen.kt`一次性加载所有医疗记录，对于记录多的用户会导致性能问题。

**解决方案**：
- 使用Room的`PagingSource`和`Pager`实现分页
- 集成Android Paging 3库
- 在UI中使用`LazyColumn`的分页加载

**涉及文件**：
- `app/src/main/java/com/yy/medtrace/data/dao/MedicalRecordDao.kt`
- `app/src/main/java/com/yy/medtrace/ui/screens/MedicalRecordScreen.kt`
- `app/build.gradle.kts`（添加Paging依赖）

### 4. HTTP客户端优化

**问题**：`GistSync.kt`中每个实例都创建新的`OkHttpClient()`，没有连接池和超时配置。

**解决方案**：
- 创建共享的`OkHttpClient`单例
- 配置连接池和超时设置
- 添加重试机制

**涉及文件**：
- `app/src/main/java/com/yy/medtrace/data/backup/GistSync.kt`

## 实施步骤

### 步骤1：主线程位图解码优化
1. 修改`MemberAvatar.kt`，使用协程在后台解码位图
2. 添加加载状态管理
3. 测试性能改进

### 步骤2：N+1查询优化
1. 在`MedicalRecordDao.kt`中添加批量查询方法
2. 更新`RecordRepository.kt`使用新方法
3. 修改`FamilyScreen.kt`使用优化后的查询

### 步骤3：分页支持
1. 添加Paging 3依赖到`build.gradle.kts`
2. 修改`MedicalRecordDao.kt`返回`PagingSource`
3. 更新`MedicalRecordScreen.kt`使用分页

### 步骤4：HTTP客户端优化
1. 创建共享的`OkHttpClient`配置
2. 修改`GistSync.kt`使用共享客户端
3. 添加超时和重试配置

## 测试策略

1. **单元测试**：为新的DAO查询方法添加测试
2. **性能测试**：使用Android Profiler验证性能改进
3. **UI测试**：验证分页加载和位图加载的用户体验
4. **集成测试**：测试完整的备份/恢复流程

## 验证标准

1. 主线程位图解码：UI帧率保持在60fps以上
2. N+1查询：数据库查询时间减少50%以上
3. 分页：内存使用减少30%以上
4. HTTP客户端：网络请求成功率提高，超时减少

## 风险和缓解措施

1. **兼容性风险**：某些优化可能影响旧设备
   - 缓解：在minSdk 24设备上充分测试

2. **内存风险**：分页可能增加内存使用
   - 缓解：合理设置页面大小，监控内存使用

3. **复杂性风险**：优化可能增加代码复杂性
   - 缓解：添加详细注释，保持代码清晰