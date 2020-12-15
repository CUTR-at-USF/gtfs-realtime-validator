-- Insert records into GtfsFeed table
SET IDENTITY_INSERT GtfsFeed ON;


INSERT INTO GtfsFeed (feedId, agency ,fileChecksum ,errorCount ,fileLocation ,feedUrl ,downloadTimestamp)
    -- Columns (feedId, agency, fileCheckSum, errorCount, fileLocation, feedUrl, downloadTimestamp)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
	(SELECT * FROM (SELECT -1 a, 'America/New_York' b, NULL c, 0 d,
	'dummy' e, 'dummy' f, 1 g)t WHERE NOT EXISTS
	(SELECT * FROM GtfsFeed WHERE feedId = -1));

SET IDENTITY_INSERT GtfsFeed OFF;
-- Insert records into GtfsRtFeed table
SET IDENTITY_INSERT GtfsRtFeed ON;

INSERT INTO GtfsRtFeed (rtFeedId, feedURL, gtfsFeedID)
    -- Columns (rtFeedId, rtFeedUrl, gtfsFeedId)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
    (SELECT * FROM (SELECT  -1 a, 'dummy' b, -1 c)t
    WHERE NOT EXISTS (SELECT * FROM GtfsRtFeed WHERE rtFeedId = -1));
SET IDENTITY_INSERT GtfsRtFeed OFF;


SET IDENTITY_INSERT GtfsRtFeedIteration ON;

-- Insert records into GtfsRtFeedIteration
INSERT INTO GtfsRtFeedIteration (iterationId, feedHash, feedTimestamp, feedProtoBuf, iterationTimestamp, rtFeedId)
-- Columns (iterationId, feedHash, feedTimestamp, feedProtoBuf, iterationTimestamp, rtFeedId)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
    (SELECT * FROM (SELECT  -2 a, NULL b, 1 c, NULL d, 1 e, -1 f) t
    WHERE NOT EXISTS (SELECT * FROM GtfsRtFeedIteration WHERE IterationId = -2));


INSERT INTO GtfsRtFeedIteration (IterationID, feedHash, feedTimestamp, feedProtobuf, IterationTimestamp, rtFeedID)
    (SELECT * FROM (SELECT -1 a, NULL b, 2 c, NULL d, 2 e, -1 f) t
    WHERE NOT EXISTS (SELECT * FROM GtfsRtFeedIteration WHERE IterationId = -1));

SET IDENTITY_INSERT GtfsRtFeedIteration OFF;
-- Insert records into MessageLog table
SET IDENTITY_INSERT MessageLog ON;
INSERT INTO MessageLog (messageId, errorDetails, iterationId, errorId)
-- Columns (messageId, errorDetails, iterationId, errorId)
    -- We ensures that record is not inserted if already exists, to avoid primary key constraint violation
    (SELECT * FROM (SELECT -6 a, NULL b, -2 c, 'W002' d) t
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -6));

INSERT INTO MessageLog (messageId, errorDetails, iterationId, errorId)
    (SELECT * FROM (SELECT -5 a, NULL b, -2 c, 'W001' d) t
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -5));

INSERT INTO MessageLog (messageId, errorDetails, iterationId, errorId)
    (SELECT * FROM (SELECT -4 a, NULL b, -2 c, 'E002' d) t
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -4));

INSERT INTO MessageLog (messageId, errorDetails, iterationId, errorId)
    (SELECT * FROM (SELECT -3 a, NULL b, -2 c, 'W002' d) t
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -3));

INSERT INTO MessageLog (messageId, errorDetails, iterationId, errorId)
    (SELECT * FROM (SELECT -2 a, NULL b, -2 c, 'W001' d) t
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -2));

INSERT INTO MessageLog (messageId, errorDetails, iterationId, errorId)
    (SELECT * FROM (SELECT -1 a, NULL b, -2 c, 'E002' d) t
    WHERE NOT EXISTS (SELECT * FROM MessageLog WHERE messageId = -1));



SET IDENTITY_INSERT MessageLog OFF;