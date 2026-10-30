package cn.iocoder.yudao.module.infra.api.file;

import jakarta.validation.constraints.NotEmpty;

/**
 * 文件 API 接口
 *
 * @author 芋道源码
 */
public interface FileApi {

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @return 文件路径
     */
    default String createFile(byte[] content) {
        return createFile(content, null, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @return 文件路径
     */
    default String createFile(byte[] content, String name) {
        return createFile(content, name, null, null);
    }

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @param directory 目录，允许空
     * @param type 文件的 MIME 类型，允许空
     * @return 文件路径
     */
    String createFile(@NotEmpty(message = "文件内容不能为空") byte[] content,
                      String name, String directory, String type);

    /**
     * 保存文件，并返回文件的完整信息（含路径、配置编号）
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @param directory 目录，允许空
     * @param type 文件的 MIME 类型，允许空
     * @return 文件上传结果（含 URL、路径、配置编号）
     */
    FileUploadRespVO createFileDetail(@NotEmpty(message = "文件内容不能为空") byte[] content,
                                      String name, String directory, String type);

    /**
     * 生成文件预签名地址，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    String presignGetUrl(@NotEmpty(message = "URL 不能为空") String url,
                         Integer expirationSeconds);

    /**
     * 将文件访问地址规范化为可长期保存的形式
     * 使用场景：私有桶上传返回的预签名 URL 存在有效期，业务入库前应先转换
     *
     * @param url 完整的文件访问地址
     * @return 可长期保存的文件地址
     */
    String normalizeUrl(String url);

    /**
     * 将已保存的文件地址，转换为当前可访问的地址
     * 使用场景：业务读取头像等私有桶文件时，按需刷新预签名 URL
     *
     * @param url 已保存的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 可访问的文件地址
     */
    String resolveUrl(String url, Integer expirationSeconds);

    /**
     * 将已保存的文件地址，转换为当前可访问的地址（默认有效期）
     *
     * @param url 已保存的文件访问地址
     * @return 可访问的文件地址
     */
    default String resolveUrl(String url) {
        return resolveUrl(url, null);
    }

    /**
     * 根据文件配置编号和存储路径，读取文件内容
     *
     * <p>用于业务模块做「同源字节流」输出（如知识库 PDF 预览），避免前端直接访问对象存储的跨域限制。</p>
     *
     * @param configId 文件配置编号（infra_file_config.id）
     * @param path 文件存储路径
     * @return 文件字节；文件不存在时返回 null
     * @throws Exception 读取存储对象失败时抛出
     */
    byte[] getFileContent(Long configId, String path) throws Exception;

    /**
     * 根据文件配置编号和存储路径，物理删除文件
     *
     * @param configId 文件配置编号（infra_file_config.id）
     * @param path 文件存储路径
     * @throws Exception 删除存储对象失败时抛出
     */
    void deleteFile(Long configId, String path) throws Exception;

}
