const TYPE_LABELS = {
  scenic: '景点',
  restaurant: '餐厅',
  hotel: '住宿',
  gas: '加油',
  rest: '休息',
  start: '出发',
  end: '终点',
  break: '休息点'
}

Component({
  properties: {
    name: { type: String, value: '' },
    type: { type: String, value: 'scenic' },
    arrival: { type: String, value: '' },
    tips: { type: String, value: '' },
    driveFromPrevKm: { type: Number, value: 0 },
    driveScore: { type: Number, value: 0 },
    isLast: { type: Boolean, value: false }
  },

  data: {
    typeLabel: ''
  },

  observers: {
    type(val) {
      this.setData({ typeLabel: TYPE_LABELS[val] || val })
    }
  }
})
