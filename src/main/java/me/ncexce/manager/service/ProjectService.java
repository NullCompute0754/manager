package me.ncexce.manager.service;

import lombok.RequiredArgsConstructor;
import me.ncexce.manager.entity.UAssetProject;
import me.ncexce.manager.entity.UAssetCommit;
import me.ncexce.manager.entity.UserEntity;
import me.ncexce.manager.exceptions.ProjectNotFoundException;
import me.ncexce.manager.repository.UAssetProjectRepository;
import me.ncexce.manager.repository.UAssetCommitRepository;
import me.ncexce.manager.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final UAssetProjectRepository projectRepository;
    private final UAssetCommitRepository commitRepository;
    private final UserRepository userRepository;

    /**
     * 创建新项目
     */
    public UAssetProject createProject(String name, String description, Long createdByUserId) {
        UserEntity createdBy = userRepository.findById(createdByUserId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        UAssetProject project = new UAssetProject();
        project.setName(name);
        project.setDescription(description);
        
        UAssetProject savedProject = projectRepository.save(project);

        // 创建初始的master commit
        createInitialMasterCommit(savedProject, createdBy);

        return savedProject;
    }

    /**
     * 获取所有项目
     */
    public List<UAssetProject> getAllProjects() {
        return projectRepository.findAll();
    }

    /**
     * 根据ID获取项目
     */
    public UAssetProject getProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException("项目不存在"));
    }

    /**
     * 删除项目
     */
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ProjectNotFoundException("项目不存在");
        }
        projectRepository.deleteById(id);
    }

    /**
     * 创建初始的master commit
     */
    private void createInitialMasterCommit(UAssetProject project, UserEntity createdBy) {
        UAssetCommit initialCommit = new UAssetCommit();
        initialCommit.setProject(project);
        initialCommit.setBranch("master");
        initialCommit.setCreatedAt(LocalDateTime.now());
        initialCommit.setMessage("项目初始提交");
        initialCommit.setCommitPath(String.format("./uploads/commits/%d/initial", project.getId()));

        commitRepository.save(initialCommit);
    }

    /**
     * 获取项目的提交历史
     */
    public List<UAssetCommit> getProjectCommits(Long projectId) {
        return commitRepository.findByProjectIdAndUserId(projectId, null); // master分支的commit
    }

    /**
     * 获取项目的用户分支列表
     */
    public List<UAssetCommit> getUserBranches(Long projectId) {
        // 获取所有有用户提交的分支
        return commitRepository.findByProjectIdAndUserId(projectId, null); // 这里需要调整查询逻辑
    }
}