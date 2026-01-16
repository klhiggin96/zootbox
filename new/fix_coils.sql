DELETE FROM coils WHERE id NOT IN ('A1','B1','C1','D1','E1','F1','G1','H1','I1','J1');
SELECT COUNT(*) FROM coils;
SELECT id, inventory FROM coils ORDER BY id;

