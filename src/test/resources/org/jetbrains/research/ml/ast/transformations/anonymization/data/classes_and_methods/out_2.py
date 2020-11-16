class c1:
    def c1f1(self):
        pass


class c2(c1):
    def c1f1(self):
        pass


(c1() if input() else c2()).c1f1()