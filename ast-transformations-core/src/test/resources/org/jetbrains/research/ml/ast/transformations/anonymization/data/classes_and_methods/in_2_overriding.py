class Base:
    def foo(self):
        pass


class Derived(Base):
    def foo(self):
        pass


(Base() if input() else Derived()).foo()