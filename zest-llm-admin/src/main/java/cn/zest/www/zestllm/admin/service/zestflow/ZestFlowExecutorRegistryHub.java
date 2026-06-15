package cn.zest.www.zestllm.admin.service.zestflow;

import cn.zest.www.zestllm.admin.model.vo.PeerExecutorVO;
import com.zestflow.common.model.dto.HeartbeatDTO;
import com.zestflow.common.model.dto.RegisterDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量 ZestFlow Hub：内存注册表，供远程 Executor 注册/心跳（合一部署 Admin 兼 Hub）。
 */
@Slf4j
@Service
public class ZestFlowExecutorRegistryHub {

    private final Map<String, ExecutorRecord> executors = new ConcurrentHashMap<>();

    public void register(RegisterDTO dto, Long tenantId) {
        if (dto == null || dto.getExecutorId() == null || dto.getExecutorId().isBlank()) {
            return;
        }
        ExecutorRecord record = new ExecutorRecord();
        record.executorId = dto.getExecutorId();
        record.host = dto.getHost();
        record.port = dto.getPort();
        record.appCode = dto.getAppCode();
        record.appName = dto.getAppName();
        record.tenantId = tenantId != null ? tenantId : 1L;
        record.status = 1;
        record.declaredChainKeys = dto.getDeclaredChainKeys() != null ? List.copyOf(dto.getDeclaredChainKeys()) : List.of();
        record.lastHeartbeatAt = Instant.now();
        executors.put(record.executorId, record);
        log.info("ZestFlow executor registered: id={} {}:{} app={}", record.executorId, record.host, record.port, record.appCode);
    }

    public void heartbeat(HeartbeatDTO dto) {
        if (dto == null || dto.getExecutorId() == null || dto.getExecutorId().isBlank()) {
            return;
        }
        ExecutorRecord record = executors.get(dto.getExecutorId());
        if (record == null) {
            record = new ExecutorRecord();
            record.executorId = dto.getExecutorId();
            record.tenantId = 1L;
            executors.put(record.executorId, record);
        }
        record.status = dto.getStatus();
        if (dto.getDeclaredChainKeys() != null) {
            record.declaredChainKeys = List.copyOf(dto.getDeclaredChainKeys());
        }
        record.lastHeartbeatAt = Instant.now();
    }

    public void deregister(String executorId) {
        if (executorId != null) {
            executors.remove(executorId);
            log.info("ZestFlow executor deregistered: id={}", executorId);
        }
    }

    public void updateStatus(String executorId, Integer status) {
        ExecutorRecord record = executors.get(executorId);
        if (record != null && status != null) {
            record.status = status;
            record.lastHeartbeatAt = Instant.now();
        }
    }

    public List<PeerExecutorVO> listOnlinePeers(String appCode) {
        List<PeerExecutorVO> peers = new ArrayList<>();
        for (ExecutorRecord record : executors.values()) {
            if (appCode != null && !appCode.isBlank() && !appCode.equals(record.appCode)) {
                continue;
            }
            PeerExecutorVO peer = new PeerExecutorVO();
            peer.setExecutorId(record.executorId);
            peer.setHost(record.host);
            peer.setPort(record.port);
            peer.setAppCode(record.appCode);
            peers.add(peer);
        }
        return peers;
    }

    public int size() {
        return executors.size();
    }

    private static final class ExecutorRecord {
        private String executorId;
        private String host;
        private int port;
        private String appCode;
        private String appName;
        private long tenantId;
        private int status;
        private List<String> declaredChainKeys = List.of();
        private Instant lastHeartbeatAt;
    }
}
