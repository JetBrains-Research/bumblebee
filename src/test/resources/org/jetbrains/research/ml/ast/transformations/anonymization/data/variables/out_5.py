def f1():
    f1v1 = 1

    def f1f1():
        nonlocal f1v1
        f1v1 = 42

    f1v2 = f1v1