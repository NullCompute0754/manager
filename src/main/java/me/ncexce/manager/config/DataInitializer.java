package me.ncexce.manager.config;

import lombok.extern.slf4j.Slf4j;
import me.ncexce.manager.entity.BranchEntity;
import me.ncexce.manager.repository.BranchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private BranchRepository branchRepo;

    @Override
    public void run(String... args) {
        log.info("开始执行系统基础环境校验...");

        // 校验全局 Master 分支
        if (!branchRepo.existsById("master")) {
            log.warn("检测到全局 Master 分支缺失，正在执行初始化...");

            BranchEntity master = new BranchEntity();
            master.setName("master");
            master.setMaster(true);
            master.setHeadCommitId(null);
            master.setUpdatedAt(LocalDateTime.now());

            branchRepo.save(master);
            log.info("全局 Master 分支初始化成功。");
        } else {
            log.info("全局 Master 分支已就绪。");
        }
    }
}
