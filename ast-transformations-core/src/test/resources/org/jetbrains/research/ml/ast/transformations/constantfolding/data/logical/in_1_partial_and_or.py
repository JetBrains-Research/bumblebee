def effect():
    print('hello')


def foo():
    return 1


pure_f_and = (0 == 1) and foo()
pure_t_and = (1 == 1) and foo()
pure_f_or = (0 == 1) or foo()
pure_t_or = (1 == 1) or foo()

# impure_f_and = ???
impure_t_and = [effect()] and foo()
# impure_f_or = ???
impure_t_or = [effect()] or foo()
