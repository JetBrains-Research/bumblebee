class Base:
    def foo(self):
        a, b = 1, 2


class Derived(Base):
    def foo(self):
        a, c = 3, 4


(Base() if input() else Derived()).foo()