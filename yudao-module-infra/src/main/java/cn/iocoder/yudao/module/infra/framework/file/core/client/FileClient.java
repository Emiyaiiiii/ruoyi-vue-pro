package cn.iocoder.yudao.module.infra.framework.file.core.client;

/**
 * 文件客户端
 *
 * @author 芋道源码
 */
public interface FileClient {

    /**
     * 获得客户端编号
     *
     * @return 客户端编号
     */
    Long getId();

    /**
     * 上传文件
     *
     * @param content 文件流
     * @param path    相对路径
     * @return 完整路径，即 HTTP 访问地址
     * @throws Exception 上传文件时，抛出 Exception 异常
     */
    String upload(byte[] content, String path, String type) throws Exception;

    /**
     * 删除文件
     *
     * @param path 相对路径
     * @throws Exception 删除文件时，抛出 Exception 异常
     */
    void delete(String path) throws Exception;

    /**
     * 获得文件的内容
     *
     * @param path 相对路径
     * @return 文件的内容
     */
    byte[] getContent(String path) throws Exception;

    // ========== 文件签名，目前仅 S3 支持 ==========

    /**
     * 获得文件预签名地址，用于上传
     *
     * @param path 相对路径
     * @return 文件预签名地址
     */
    default String presignPutUrl(String path) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 生成文件预签名地址，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    default String presignGetUrl(String url, Integer expirationSeconds) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 将文件的访问地址，规范化为可长期保存的形式
     * 使用场景：私有桶上传返回的预签名 URL 存在有效期，业务入库前应转换为相对路径
     *
     * @param url 完整的文件访问地址
     * @return 可长期保存的文件地址；无法识别时原样返回
     */
    default String normalizeUrl(String url) {
        return url;
    }

    /**
     * 将已保存的文件地址，转换为当前可访问的地址
     * 使用场景：私有桶读取时，基于相对路径重新生成预签名 URL
     *
     * @param url 已保存的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 可访问的文件地址；不支持签名时原样返回
     */
    default String resolveUrl(String url, Integer expirationSeconds) {
        return url;
    }

}
