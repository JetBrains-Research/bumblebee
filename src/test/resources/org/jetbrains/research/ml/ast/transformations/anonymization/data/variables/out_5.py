def f1():
    f1_v1 = 1

    def f1_f1():
        nonlocal f1_v1
        f1_v1 = 42

    f1_v2 = f1_v1