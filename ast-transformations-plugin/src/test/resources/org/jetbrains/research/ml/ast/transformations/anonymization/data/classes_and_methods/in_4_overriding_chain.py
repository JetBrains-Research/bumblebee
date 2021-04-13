class A:
    def foo(self):
        pass


class B(A):
    def foo(self):
        pass


class C(B):
    def foo(self):
        pass

    def bar(self):
        pass


class D(C):
    def foo(self):
        pass

    def bar(self):
        pass