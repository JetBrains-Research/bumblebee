class c1:
    def c1_f1(self):
        pass


class c2(c1):
    def c1_f1(self):
        pass


class c3(c2):
    def c1_f1(self):
        pass

    def c3_f1(self):
        pass


class c4(c3):
    def c1_f1(self):
        pass

    def c3_f1(self):
        pass