-- 完成榜按状态筛选、按接单者聚合；覆盖这两个查询列。
CREATE INDEX idx_mission_status_acceptor ON t_mission (status, acceptor_id);
