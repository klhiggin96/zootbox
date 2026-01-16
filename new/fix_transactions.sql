DELETE FROM transactions WHERE coil_id NOT IN ('A1','B1','C1','D1','E1','F1','G1','H1','I1','J1');
SELECT COUNT(*) as remaining_transactions FROM transactions;

