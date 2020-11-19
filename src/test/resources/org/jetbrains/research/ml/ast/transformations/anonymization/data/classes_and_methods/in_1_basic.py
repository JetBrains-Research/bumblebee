class C:
    def __init__(self):
        pass

    def foo(self, foo, bar):
        print(foo, bar, self)

    def bar(self):
        pass

    @classmethod
    def class_baz(cls, x):
        pass

    @staticmethod
    def static_yep(a, b, c):
        pass