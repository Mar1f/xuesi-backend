-- 删除重复的题目记录，保留最新的记录
DELETE qbq1 FROM question_bank_question qbq1
INNER JOIN question_bank_question qbq2
WHERE qbq1.questionBankId = qbq2.questionBankId
AND qbq1.questionId = qbq2.questionId
AND qbq1.createTime < qbq2.createTime; 