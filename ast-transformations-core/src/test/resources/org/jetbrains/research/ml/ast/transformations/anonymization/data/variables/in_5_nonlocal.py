def foo():
    var = 1

    def bar():
        nonlocal var
        var = 42

    i = var