-- Copyright 2020 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

---------- KUBERNETES_UTILIZATION_DATA TABLE START ------------
BEGIN;
CREATE TABLE IF NOT EXISTS KUBERNETES_UTILIZATION_DATA (
	STARTTIME TIMESTAMPTZ NOT NULL,
	ENDTIME TIMESTAMPTZ NOT NULL,
	ACCOUNTID TEXT NOT NULL,
	SETTINGID TEXT NOT NULL,
	INSTANCEID TEXT NOT NULL,
	INSTANCETYPE TEXT NOT NULL,
	CPU DOUBLE PRECISION  NOT NULL,
	MEMORY DOUBLE PRECISION  NOT NULL
);
COMMIT;
SELECT CREATE_HYPERTABLE('KUBERNETES_UTILIZATION_DATA','starttime',if_not_exists => TRUE);

BEGIN;
CREATE INDEX IF NOT EXISTS KUBERNETES_UTILIZATION_DATA_INSTANCEID_INDEX ON KUBERNETES_UTILIZATION_DATA(INSTANCEID, STARTTIME DESC);
COMMIT;
---------- KUBERNETES_UTILIZATION_DATA TABLE END ------------
