x = int(input())
b = bool(input())

int_one_int_op_int_id = x + 0
int_one_int_op_bool_id = x + False
int_one_bool_op_int_id = x | 0
int_one_bool_op_bool_id = x | False
int_many_int_op_int_id = x + x + 0
int_many_int_op_bool_id = x + x + False
int_many_bool_op_int_id = x | x | 0
int_many_bool_op_bool_id = x | x | False

bool_one_int_op_int_id = b + 0
bool_one_int_op_bool_id = b + False
bool_one_bool_op_int_id = b | 0
bool_one_bool_op_bool_id = b | False
bool_many_int_op_int_id = b + b + 0
bool_many_int_op_bool_id = b + b + False
bool_many_bool_op_int_id = b | b | 0
bool_many_bool_op_bool_id = b | b | False