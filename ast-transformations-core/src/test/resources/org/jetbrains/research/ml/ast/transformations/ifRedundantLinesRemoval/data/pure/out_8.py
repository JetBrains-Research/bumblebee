def pure_foo(s, y):
    x = 1
    y = x
    print(y)
    if s:
        print('foo')
    else:
        print('bar')
