def foo(a):
    if a <= 1:
        return 1
    return foo(a - 1) * foo(a)