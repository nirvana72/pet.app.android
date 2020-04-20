package com.mu78.pethobby.modules

class Article(
    val Id: Int,
    val title: String,
    val type: String,
    val authorId: Int,
    val writetime: String,
    var likes: Int,
    var lauds: Int,
    val comments: Int,
    val postAddr: String,
    val authorname: String,
    val avatar: Int,
    val abstract: String,
    val videos: List<OSSVideo>,
    val images: List<String>,
    var liked: Boolean = false,
    var lauded: Boolean = false,
    var status: Int,
    val subscribe:Boolean = false
) {
    enum class Status(val value:Int) {
        Create(11), // 提交第一步，出错会停留在这一步
        Review(1),  // 正常情况下，提交等街上传资源完成，变成待审核
        Publish(2), // 审核通过
        Reject(10), // 审核被拒
        Delete(-1), // 已删除， 文章表中已删除，其它表中还有引用的情况
    }
}

class OSSVideo(val fname: String, val duration: Int)

class BaseApiResult(val ret:Int, val msg: String)

// 检查更新返回格式
class CheckUpdateApiResult(var version: String, var apkurl: String, var info: Array<String>)