
#ifndef PCMPLAY_PCMDATA_H
#define PCMPLAY_PCMDATA_H


class PcmData {
public:
    char *data;
    int size;

public:
    PcmData(char *data, int size);

    ~PcmData();

    int getSize();

    char *getData();
};


#endif //PCMPLAY_PCMDATA_H
