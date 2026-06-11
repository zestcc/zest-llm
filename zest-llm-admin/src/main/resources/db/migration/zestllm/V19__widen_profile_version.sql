-- Widen profile/prompt version columns for template wizard and long version names
ALTER TABLE llm_agent_profile MODIFY COLUMN version VARCHAR(64) NOT NULL;
ALTER TABLE llm_agent_profile_probe MODIFY COLUMN profile_version VARCHAR(64);
ALTER TABLE llm_agent_probe_alert MODIFY COLUMN profile_version VARCHAR(64);
ALTER TABLE llm_learning_cycle_run MODIFY COLUMN profile_version VARCHAR(64) NOT NULL;
ALTER TABLE llm_prompt_version MODIFY COLUMN version VARCHAR(64) NOT NULL;
ALTER TABLE llm_execution_archive_run MODIFY COLUMN prompt_version VARCHAR(64);
