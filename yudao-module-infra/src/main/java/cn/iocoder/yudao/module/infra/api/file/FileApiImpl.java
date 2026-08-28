package cn.iocoder.yudao.module.infra.api.file;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FilePathUtils;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.hutool.core.lang.Assert;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.Resource;

/**
 * 文件 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Resource
    private FileConfigService fileConfigService;

    @Override
    public String createFile(byte[] content, String name, String directory, String type) {
        return fileService.createFile(content, name, directory, type);
    }

    @Override
    public FileUploadRespVO createFileDetail(byte[] content, String name, String directory, String type) {
        return fileService.createFileDetail(content, name, directory, type);
    }

    @Override
    public String presignGetUrl(String url, Integer expirationSeconds) {
        return fileService.presignGetUrl(url, expirationSeconds);
    }

    @Override
    public void deleteFile(Long configId, String path) throws Exception {
        // 校验路径合法性，避免误删文件存储器中的其他文件
        FilePathUtils.validatePath(path);
        FileClient client = fileConfigService.getFileClient(configId);
        Assert.notNull(client, "文件配置({}) 对应客户端不能为空", configId);
        client.delete(path);
    }

}
