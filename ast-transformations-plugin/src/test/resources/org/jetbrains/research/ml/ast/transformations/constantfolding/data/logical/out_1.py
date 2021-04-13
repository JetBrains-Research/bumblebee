def effect():
    print("hello")


def foo():
    return 1


pure_f_and = False
pure_t_and = foo()
pure_f_or = foo()
pure_t_or = True

# impure_f_and = ???
impure_t_and = [effect()] and foo()
# impure_f_or = ???
impure_t_or = [effect()]
