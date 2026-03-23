CREATE DATABASE IF NOT EXISTS qa_sys_jpa DEFAULT CHARACTER SET utf8mb4;
USE qa_sys_jpa;

-- 1. 用户表
CREATE TABLE `sys_user` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `username` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
                            `password` VARCHAR(255) NOT NULL COMMENT '密码',
                            `status` INT DEFAULT 1 COMMENT '1:正常, 0:停用',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='用户表';

-- 2. 角色表
CREATE TABLE `sys_role` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT,
                            `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
                            `role_code` VARCHAR(64) NOT NULL UNIQUE COMMENT '角色标识(ROLE_ADMIN)',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='角色表';

-- 3. 权限菜单表
CREATE TABLE `sys_permission` (
                                  `id` BIGINT NOT NULL AUTO_INCREMENT,
                                  `parent_id` BIGINT DEFAULT 0 COMMENT '父节点ID',
                                  `perm_name` VARCHAR(64) NOT NULL COMMENT '权限名称',
                                  `perm_code` VARCHAR(100) COMMENT '权限标识(sys:userDTO:add)',
                                  `type` INT NOT NULL COMMENT '1:目录, 2:菜单, 3:按钮',
                                  `path` VARCHAR(255) COMMENT '前端路由',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='权限表';

-- 4. 用户-角色关联表 (带级联外键)
CREATE TABLE `sys_user_role` (
                                 `user_id` BIGINT NOT NULL,
                                 `role_id` BIGINT NOT NULL,
                                 PRIMARY KEY (`user_id`, `role_id`),
                                 CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE,
                                 CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='用户角色关联表';

-- 5. 角色-权限关联表 (带级联外键)
CREATE TABLE `sys_role_permission` (
                                       `role_id` BIGINT NOT NULL,
                                       `permission_id` BIGINT NOT NULL,
                                       PRIMARY KEY (`role_id`, `permission_id`),
                                       CONSTRAINT `fk_rp_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
                                       CONSTRAINT `fk_rp_perm` FOREIGN KEY (`permission_id`) REFERENCES `sys_permission` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='角色权限关联表';